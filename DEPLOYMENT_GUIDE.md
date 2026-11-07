# Oracle Cloud Infrastructure (OCI) Deployment Guide
## Autonomous Vehicle (AV) Fleet Safety & Telemetry Audit Engine

> **Academic Collaboration Project (Set 21)**  
> **Architecture Pattern:** Multi-tier Distributed Cloud Deployment  
> - **VM 1 (Student 1):** Application Host (Java Runtime, Multi-threaded Engine, Telemetry Ingestion, Report Exporter)  
> - **VM 2 (Student 2):** Database Host (MySQL Community Server 8.0+, `av_fleet_db` schema, restricted remote access)  
> **Zero Public Exposure Policy:** VM 2 does NOT expose port 3306 to the public internet; traffic is routed exclusively across OCI VCN Private IP addresses (or tightly bound Security List ingress rules).

---

## Table of Contents
1. [Prerequisites & OCI Provisioning](#1-prerequisites--oci-provisioning)
2. [Network & Firewall Security Architecture](#2-network--firewall-security-architecture)
   - [A. OCI VCN Security List Configuration](#a-oci-vcn-security-list-configuration)
   - [B. Linux OS Firewall (iptables / firewalld / ufw)](#b-linux-os-firewall-iptables--firewalld--ufw)
3. [VM 2: Database Host Configuration (Student 2)](#3-vm-2-database-host-configuration-student-2)
   - [Step 2.1: MySQL Server Installation](#step-21-mysql-server-installation)
   - [Step 2.2: MySQL Remote Binding (my.cnf)](#step-22-mysql-remote-binding-mycnf)
   - [Step 2.3: Database Schema & Dedicated User Setup](#step-23-database-schema--dedicated-user-setup)
   - [Step 2.4: OS Firewall Rule Configuration](#step-24-os-firewall-rule-configuration)
4. [VM 1: Application Host Configuration (Student 1)](#4-vm-1-application-host-configuration-student-1)
   - [Step 1.1: Java 17/21 & Git Installation](#step-11-java-1721--git-installation)
   - [Step 1.2: Repository Cloning & Dependency Verification](#step-12-repository-cloning--dependency-verification)
   - [Step 1.3: Environment Variable Configuration (.env)](#step-13-environment-variable-configuration-env)
   - [Step 1.4: Compilation & Build](#step-14-compilation--build)
5. [End-to-End Connectivity & System Verification](#5-end-to-end-connectivity--system-verification)
6. [Operational Maintenance & Troubleshooting](#6-operational-maintenance--troubleshooting)

---

## 1. Prerequisites & OCI Provisioning

### A. OCI Compute Instance Specifications (Always Free Compatible)
- **Shape:** `VM.Standard.E2.1.Micro` (AMD) or `VM.Standard.A1.Flex` (Ampere ARM, 2 OCPUs, 12GB RAM)
- **Operating System:** Oracle Linux 8/9 or Ubuntu 22.04 LTS
- **VCN Topology:** Both VMs placed in the same OCI Virtual Cloud Network (VCN) and regional Subnet (recommended) or peered VCNs.

| Machine | Role | Hostname / Label | Recommended Private IP Example | Public IP (SSH Bastion) |
|---|---|---|---|---|
| **VM 1** | Application Server | `av-app-node-01` | `10.0.0.15` | `129.xxx.xxx.xxx` (Ephemeral/Reserved) |
| **VM 2** | Database Server | `av-db-node-01` | `10.0.0.25` | `132.xxx.xxx.xxx` (Or private-only) |

---

## 2. Network & Firewall Security Architecture

```
[ Application VM 1 ] (Private IP: 10.0.0.15)
         │
         │  TCP Request -> Port 3306
         ▼
[ OCI VCN Ingress Security List ]
   Condition: Source = 10.0.0.15/32, Protocol = TCP, Destination Port = 3306
         │
         ▼
[ VM 2 Linux OS Firewall (iptables / firewalld) ]
   Condition: Accept TCP 3306 from 10.0.0.15 only
         │
         ▼
[ MySQL Daemon (bind-address = 0.0.0.0) ]
   User: 'av_user'@'10.0.0.15' (Encrypted Password Auth)
         │
         ▼
[ Database: av_fleet_db ]
```

### A. OCI VCN Security List Configuration

1. In the **OCI Console**, navigate to **Networking -> Virtual Cloud Networks -> Select your VCN -> Security Lists**.
2. Select the **Default Security List for your Subnet** (or create a dedicated Database Security List).
3. Click **Add Ingress Rules**:
   - **Stateless:** Unchecked (Stateful rule)
   - **Source Type:** CIDR
   - **Source CIDR:** `<VM_1_PRIVATE_IP>/32` (e.g., `10.0.0.15/32`)  
     *(Never use `0.0.0.0/0` for database ingress)*
   - **IP Protocol:** TCP
   - **Source Port Range:** All
   - **Destination Port Range:** `3306`
   - **Description:** `Allow MySQL JDBC traffic from AV Fleet Safety Application VM 1`
4. Click **Add Ingress Rules**.

---

## 3. VM 2: Database Host Configuration (Student 2)

Log in to **VM 2** via SSH:
```bash
ssh -i /path/to/private_key opc@<VM_2_PUBLIC_IP>      # For Oracle Linux
# OR
ssh -i /path/to/private_key ubuntu@<VM_2_PUBLIC_IP>   # For Ubuntu
```

### Step 2.1: MySQL Server Installation

#### For Oracle Linux / RHEL:
```bash
sudo dnf update -y
sudo dnf install -y @mysql:8.0
sudo systemctl enable --now mysqld
sudo systemctl status mysqld
```

#### For Ubuntu / Debian:
```bash
sudo apt update && sudo apt upgrade -y
sudo apt install -y mysql-server
sudo systemctl enable --now mysql
sudo systemctl status mysql
```

---

### Step 2.2: MySQL Remote Binding (`my.cnf`)

By default, MySQL listens only on `127.0.0.1` (localhost). We must configure it to listen on the private network interface while relying on OCI Security Lists and firewalls for isolation.

1. Edit MySQL configuration:
   - **Oracle Linux:** `sudo nano /etc/my.cnf.d/mysql-server.cnf`
   - **Ubuntu:** `sudo nano /etc/mysql/mysql.conf.d/mysqld.cnf`

2. Locate or add `bind-address` under the `[mysqld]` section:
   ```ini
   [mysqld]
   bind-address = 0.0.0.0
   # Recommended for cloud integrity:
   max_connections = 150
   default_time_zone = '+00:00'
   ```

3. Restart the MySQL service:
   ```bash
   # Oracle Linux:
   sudo systemctl restart mysqld

   # Ubuntu:
   sudo systemctl restart mysql
   ```

4. Verify listener:
   ```bash
   sudo ss -tulpn | grep 3306
   # Expected output: LISTEN 0 128 *:3306 ...
   ```

---

### Step 2.3: Database Schema & Dedicated User Setup

1. Copy [`database/av_fleet_schema.sql`](database/av_fleet_schema.sql) to VM 2.
2. Log into the MySQL administrative console:
   ```bash
   sudo mysql -u root
   ```

3. Create the database and execute the schema:
   ```sql
   CREATE DATABASE IF NOT EXISTS av_fleet_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   USE av_fleet_db;
   SOURCE /path/to/av_fleet_schema.sql;
   SHOW TABLES;
   ```

4. Create the restricted dedicated user for Student 1 (Application VM 1):
   > Replace `<VM_1_PRIVATE_IP>` with VM 1's actual private IP (e.g., `10.0.0.15`), and `<STRONG_PASSWORD>` with your generated password.
   ```sql
   -- Create user permitted exclusively from VM 1 Private IP
   CREATE USER 'av_user'@'<VM_1_PRIVATE_IP>' IDENTIFIED BY '<STRONG_PASSWORD>';

   -- Grant permissions to av_fleet_db only
   GRANT SELECT, INSERT, UPDATE, DELETE ON av_fleet_db.* TO 'av_user'@'<VM_1_PRIVATE_IP>';

   -- Optional: If testing from VM 2 localhost as well
   CREATE USER 'av_user'@'localhost' IDENTIFIED BY '<STRONG_PASSWORD>';
   GRANT SELECT, INSERT, UPDATE, DELETE ON av_fleet_db.* TO 'av_user'@'localhost';

   FLUSH PRIVILEGES;
   SELECT user, host FROM mysql.user WHERE user = 'av_user';
   EXIT;
   ```

---

### Step 2.4: Linux OS Firewall Rule Configuration

#### For Oracle Linux (firewalld):
```bash
# Add permanent rule restricted to VM 1 Private IP
sudo firewall-cmd --permanent --zone=public --add-rich-rule='rule family="ipv4" source address="<VM_1_PRIVATE_IP>/32" port protocol="tcp" port="3306" accept'
sudo firewall-cmd --reload
sudo firewall-cmd --list-all
```

#### For Ubuntu (UFW):
```bash
sudo ufw allow from <VM_1_PRIVATE_IP> to any port 3306 proto tcp
sudo ufw status verbose
```

#### For Linux `iptables` directly:
```bash
sudo iptables -I INPUT -p tcp -s <VM_1_PRIVATE_IP> --dport 3306 -j ACCEPT
sudo iptables-save | sudo tee /etc/sysconfig/iptables
```

---

## 4. VM 1: Application Host Configuration (Student 1)

Log in to **VM 1** via SSH:
```bash
ssh -i /path/to/private_key opc@<VM_1_PUBLIC_IP>      # For Oracle Linux
# OR
ssh -i /path/to/private_key ubuntu@<VM_1_PUBLIC_IP>   # For Ubuntu
```

### Step 1.1: Java 17/21 & Git Installation

#### Oracle Linux:
```bash
sudo dnf update -y
sudo dnf install -y git java-17-openjdk-devel
java -version
javac -version
```

#### Ubuntu:
```bash
sudo apt update && sudo apt upgrade -y
sudo apt install -y git openjdk-17-jdk
java -version
javac -version
```

---

### Step 1.2: Repository Cloning & Dependency Verification

1. Clone or sync the project repository:
   ```bash
   git clone <YOUR_GIT_REPO_URL> av_fleet_safety_audit
   cd av_fleet_safety_audit
   ```

2. Verify project structure:
   ```bash
   ls -la
   # Ensure src/, database/, data/, lib/, reports/, and bin/ directories exist
   mkdir -p bin reports
   ```

3. Verify JDBC Driver presence in `lib/`:
   ```bash
   ls -la lib/mysql-connector-j-*.jar
   # If missing, download directly:
   # curl -L -o lib/mysql-connector-j-8.3.0.jar https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.3.0/mysql-connector-j-8.3.0.jar
   ```

---

### Step 1.3: Environment Variable Configuration (`.env`)

Never commit plain passwords to version control. Set environment variables on VM 1:

1. Create a secure local `.env` file (which is git-ignored):
   ```bash
   cat << 'EOF' > .env
   export DB_HOST="<VM_2_PRIVATE_IP>"
   export DB_PORT="3306"
   export DB_NAME="av_fleet_db"
   export DB_USER="av_user"
   export DB_PASS="<STRONG_PASSWORD>"
   EOF
   chmod 600 .env
   ```

2. Load the variables into your shell session:
   ```bash
   source .env
   echo "DB_HOST is set to: $DB_HOST"
   ```

---

### Step 1.4: Compilation & Build

Compile all Java source code using the included MySQL JDBC connector:

```bash
javac -cp "lib/*" -d bin src/*.java
```

Verify compiled classes:
```bash
ls -la bin/
```

---

## 5. End-to-End Connectivity & System Verification

Execute the verification steps in order:

### 1. Direct Network & Port Test:
Verify that VM 1 can reach VM 2 on TCP port 3306:
```bash
nc -zv $DB_HOST 3306
# Expected output: Connection to 10.0.0.25 3306 port [tcp/mysql] succeeded!
```

### 2. Test JDBC Connection:
```bash
java -cp "bin:lib/*" DBConnection
```
**Expected Successful Banner:**
```text
================================
AV FLEET DATABASE CONNECTION
================================
Connecting to MySQL...
Database: av_fleet_db
Connection Status: SUCCESS
JDBC Connection Established.
================================
```

### 3. Run Automated Integration Test Suite:
```bash
java -cp "bin:lib/*" MainApp --test-all
```
This tests all 12 modules end-to-end:
- Vehicle registration (VIN regex validation).
- Pre-trip sensor inspection (LiDAR, Radar, Camera).
- Telemetry ingestion from `data/telemetry_logs.csv` and MySQL persistence.
- Post-trip disengagement rate calculations.
- Category-specific ASI mathematical evaluations.
- Safety quarantine enforcement and duplicate lockout.
- 100+ AV multithreaded simulation (`FixedThreadPool`).
- Audit reports export to `reports/fleet_safety_report.txt` and `reports/quarantine_report.txt`.
- Safety certificate generation (`reports/safety_certificate_AV001.txt`).

### 4. Run Interactive CLI Menu:
```bash
java -cp "bin:lib/*" MainApp
```

---

## 6. Operational Maintenance & Troubleshooting

| Issue | Root Cause | Solution |
|---|---|---|
| `Communications link failure` | VM 2 MySQL not listening on network or OCI Security List blocked. | 1. Check `sudo ss -tulpn \| grep 3306` on VM 2.<br>2. Check OCI Ingress Security List for Port 3306 from VM 1 Private IP.<br>3. Check `firewalld` or `ufw` rules on VM 2. |
| `Access denied for user 'av_user'@'10.0.0.15'` | Password mismatch or MySQL user host is restricted to `localhost`. | On VM 2 run: `SELECT user, host FROM mysql.user;`. Ensure user is created as `'av_user'@'<VM_1_PRIVATE_IP>'`. |
| `ClassNotFoundException: com.mysql.cj.jdbc.Driver` | Classpath flag missing or wrong separator. | On Linux use colon `:` instead of semicolon `;`: `java -cp "bin:lib/*" MainApp`. |
| `Data truncation` / `Table doesn't exist` | DDL schema not sourced. | Sourced `database/av_fleet_schema.sql` on VM 2 MySQL. |
