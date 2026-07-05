# ─────────────────────────────────────────────────────────────────────────────
# VPC dell'ambiente — scelta cost-min (#06 7): subnet PUBBLICHE + security group
# stretti, NIENTE NAT Gateway (~32 $/mese risparmiati per ambiente).
#   • uscita verso internet (Paddle, Cognito, ECR pull, …) via Internet Gateway;
#   • ingresso SOLO dagli ingressi gestiti (API Gateway); i security group dei
#     servizi (UC 0004) non aprono porte al mondo;
#   • servizi AWS interni raggiunti via VPC endpoint (endpoints.tf).
# Hardening a subnet private + NAT = evoluzione E1 (docs/_EVOLUZIONI-DEVOPS.md).
#
# Due subnet in due zone di disponibilità: richiesto da Aurora (subnet group
# multi-AZ) anche con una sola istanza; i task ECS restano 1 per servizio (E3).
# ─────────────────────────────────────────────────────────────────────────────

data "aws_availability_zones" "available" {
  state = "available"
}

resource "aws_vpc" "this" {
  cidr_block           = var.vpc_cidr
  enable_dns_support   = true
  enable_dns_hostnames = true # richiesto dai VPC endpoint con private DNS

  #checkov:skip=CKV2_AWS_11:VPC flow logs disattivati (cost-min PoC); si accendono con l'hardening E1

  tags = {
    Name = "appgrove-${var.env}"
  }
}

resource "aws_subnet" "public" {
  count = 2

  vpc_id            = aws_vpc.this.id
  cidr_block        = cidrsubnet(var.vpc_cidr, 8, count.index)
  availability_zone = data.aws_availability_zones.available.names[count.index]

  # Niente IP pubblico automatico a livello di subnet: chi ne ha bisogno
  # (i task ECS, per l'uscita via IGW senza NAT) lo richiede esplicitamente
  # sulla propria interfaccia (assign_public_ip, UC 0004).
  map_public_ip_on_launch = false

  tags = {
    Name = "appgrove-${var.env}-public-${data.aws_availability_zones.available.names[count.index]}"
  }
}

resource "aws_internet_gateway" "this" {
  vpc_id = aws_vpc.this.id

  tags = {
    Name = "appgrove-${var.env}"
  }
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.this.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.this.id
  }

  tags = {
    Name = "appgrove-${var.env}-public"
  }
}

resource "aws_route_table_association" "public" {
  count = length(aws_subnet.public)

  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

# Il security group di DEFAULT della VPC viene svuotato (nessuna regola in
# ingresso né in uscita): ogni componente usa un security group esplicito e
# stretto. Buona pratica raccomandata (e verificata da checkov).
resource "aws_default_security_group" "default" {
  vpc_id = aws_vpc.this.id
  # nessun blocco ingress/egress = tutto negato

  tags = {
    Name = "appgrove-${var.env}-default-DENY"
  }
}
