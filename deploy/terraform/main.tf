terraform {
  required_version = ">= 1.2.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

# Fetch latest Ubuntu 22.04 LTS AMI
data "aws_ami" "ubuntu" {
  most_recent = true
  owners      = ["099720109477"] # Canonical

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-amd64-server-*"]
  }
}

# Security Group allowing HTTP REST / Vector API (8080) and SSH (22)
resource "aws_security_group" "syntricdb_sg" {
  name        = "syntricdb-cloud-sg"
  description = "SyntricDB Cloud Database Security Group"

  ingress {
    description = "HTTP API & Web Dashboard"
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "SSH Access"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# Provision AWS EC2 instance running Dockerized SyntricDB
resource "aws_instance" "syntricdb_server" {
  ami                         = data.aws_ami.ubuntu.id
  instance_type               = var.instance_type
  vpc_security_group_ids      = [aws_security_group.syntricdb_sg.id]
  key_name                    = var.key_name != "" ? var.key_name : null
  associate_public_ip_address = true

  root_block_device {
    volume_size = 30
    volume_type = "gp3"
  }

  user_data = <<-EOF
              #!/bin/bash
              set -e
              apt-get update
              apt-get install -y docker.io docker-compose
              systemctl enable --now docker
              
              # Run SyntricDB Production Container
              docker run -d \
                --name syntricdb-server \
                --restart unless-stopped \
                -p 8080:8080 \
                -e SYNTRICDB_BIND_ADDRESS=0.0.0.0 \
                -e SYNTRICDB_PORT=8080 \
                -e SYNTRICDB_ADMIN_USER=admin \
                -e SYNTRICDB_ADMIN_PASSWORD="${var.admin_password}" \
                -v syntricdb_data:/var/lib/syntricdb \
                ghcr.io/upendra-manike/syntricdb:latest
              EOF

  tags = {
    Name = "SyntricDB-Cloud-Database"
  }
}
