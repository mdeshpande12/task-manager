#!/bin/bash
# Run this script on a fresh Amazon Linux 2023 EC2 instance
# Usage: ssh into EC2, then: bash setup-ec2.sh

set -e

echo "=== Installing Java 11 ==="
sudo yum install -y java-11-amazon-corretto-headless

echo "=== Creating app directory ==="
sudo mkdir -p /opt/task-manager
sudo chown ec2-user:ec2-user /opt/task-manager

echo "=== Creating environment file ==="
cat > /opt/task-manager/.env << 'EOF'
# Database (Neon PostgreSQL)
# These are read by config.yml via environment variable substitution
DB_URL=jdbc:postgresql://ep-old-bread-aqf54pir.c-8.us-east-1.aws.neon.tech/neondb?sslmode=require
DB_USER=neondb_owner
DB_PASSWORD=<YOUR_DB_PASSWORD>

# Datadog
DD_API_KEY=<YOUR_DD_API_KEY>
EOF

echo "=== Installing systemd service ==="
sudo cp /opt/task-manager/task-manager.service /etc/systemd/system/ 2>/dev/null || \
  echo "NOTE: Copy task-manager.service to /etc/systemd/system/ manually"
sudo systemctl daemon-reload
sudo systemctl enable task-manager

echo "=== Installing Datadog Agent ==="
# Replace <YOUR_DD_API_KEY> with your actual key
DD_API_KEY="<YOUR_DD_API_KEY>" DD_SITE="datadoghq.com" bash -c "$(curl -L https://install.datadoghq.com/scripts/install_script_agent7.sh)"

echo ""
echo "=== Setup complete! ==="
echo "Next steps:"
echo "  1. Edit /opt/task-manager/.env with real credentials"
echo "  2. Copy your JAR:  scp target/task-manager-1.0-SNAPSHOT.jar ec2-user@<host>:/opt/task-manager/task-manager.jar"
echo "  3. Copy config:    scp config.yml ec2-user@<host>:/opt/task-manager/config.yml"
echo "  4. Copy service:   scp deploy/task-manager.service ec2-user@<host>:/opt/task-manager/"
echo "     Then:           sudo cp /opt/task-manager/task-manager.service /etc/systemd/system/"
echo "     Then:           sudo systemctl daemon-reload"
echo "  5. Start:          sudo systemctl start task-manager"
echo "  6. Check:          sudo systemctl status task-manager"
echo "  7. Logs:           sudo journalctl -u task-manager -f"
