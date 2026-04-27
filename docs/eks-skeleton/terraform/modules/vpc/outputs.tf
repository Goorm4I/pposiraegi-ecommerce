output "vpc_id" {
  description = "VPC ID"
  value       = aws_vpc.main.id
}

output "vpc_cidr_block" {
  description = "VPC CIDR 블록"
  value       = aws_vpc.main.cidr_block
}

output "public_subnet_ids" {
  description = "퍼블릭 서브넷 ID 목록"
  value       = [for subnet in aws_subnet.public : subnet.id]
}

output "public_subnet_ids_map" {
  description = "퍼블릭 서브넷 ID 맵"
  value       = { for k, v in aws_subnet.public : k => v.id }
}

output "private_subnet_ids" {
  description = "프라이빗 서브넷 ID 목록"
  value       = [for subnet in aws_subnet.private : subnet.id]
}

output "private_subnet_ids_map" {
  description = "프라이빗 서브넷 ID 맵"
  value       = { for k, v in aws_subnet.private : k => v.id }
}

output "internet_gateway_id" {
  description = "Internet Gateway ID"
  value       = aws_internet_gateway.igw.id
}

output "nat_gateway_id" {
  description = "NAT Gateway ID"
  value       = aws_nat_gateway.nat.id
}

output "nat_public_ip" {
  description = "NAT Gateway 공용 IP"
  value       = aws_eip.nat.public_ip
}

output "availability_zones" {
  description = "사용 중인 Availability Zones"
  value       = [for subnet in aws_subnet.public : subnet.availability_zone]
}
