output "public_ip" {
  description = "Public IP Address of SyntricDB Cloud Server"
  value       = aws_instance.syntricdb_server.public_ip
}

output "connection_uri" {
  description = "SyntricDB Connection URI"
  value       = "syntricdb://admin:${var.admin_password}@${aws_instance.syntricdb_server.public_ip}:8080/default"
}

output "web_dashboard_url" {
  description = "Web Management Studio Console URL"
  value       = "http://${aws_instance.syntricdb_server.public_ip}:8080/"
}
