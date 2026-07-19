# ─────────────────────────────────────────────────────────────────────────────
# VPC endpoint "interface" (#06 18): con la scelta no-NAT, i componenti in VPC
# raggiungono Cognito, Secrets Manager, Parameter Store e SES attraverso questi
# endpoint privati (~7 $/mese l'uno → ~29 $/mese/ambiente, già in _COSTI-AWS).
#
# Cost-min: UNA sola subnet/AZ per endpoint. Il private DNS vale per tutta la
# VPC (il traffico cross-AZ ha un costo trascurabile ai volumi del PoC);
# la ridondanza multi-AZ segue l'hardening E1/E3.
# ─────────────────────────────────────────────────────────────────────────────

resource "aws_security_group" "vpc_endpoints" {
  name        = "appgrove-${var.env}-vpc-endpoints"
  description = "Accesso HTTPS agli endpoint VPC dai soli indirizzi della VPC"
  vpc_id      = aws_vpc.this.id

  ingress {
    description = "HTTPS dalla VPC verso gli endpoint"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = [aws_vpc.this.cidr_block]
  }

  # Nessuna regola egress: gli endpoint ricevono soltanto.

  tags = {
    Name = "appgrove-${var.env}-vpc-endpoints"
  }
}

locals {
  # Servizi raggiunti via endpoint privato con la topologia no-NAT (#06 18).
  # `ssm` aggiunto da UC 0015: la Lambda BFF auth (in VPC, niente uscita
  # internet dalle ENI) legge il client secret Cognito da Parameter Store.
  # `email` aggiunto da UC 0018: l'email di INVITO è l'unica che parte dal nostro
  # backend (verifica e reimpostazione le spedisce Cognito, da fuori la nostra
  # rete). Senza questo endpoint la chiamata a SES resta appesa fino al timeout.
  # Rimuoverlo è possibile solo facendo generare anche l'invito a Cognito, cosa
  # che richiede di riprogettare il flusso inviti (evoluzione E24).
  interface_endpoints = ["cognito-idp", "secretsmanager", "ssm", "email"]
}

data "aws_region" "current" {}

resource "aws_vpc_endpoint" "interface" {
  for_each = toset(local.interface_endpoints)

  vpc_id              = aws_vpc.this.id
  service_name        = "com.amazonaws.${data.aws_region.current.region}.${each.key}"
  vpc_endpoint_type   = "Interface"
  subnet_ids          = [aws_subnet.public[0].id] # una sola AZ: cost-min (vedi testata)
  security_group_ids  = [aws_security_group.vpc_endpoints.id]
  private_dns_enabled = true

  tags = {
    Name = "appgrove-${var.env}-${each.key}"
  }
}
