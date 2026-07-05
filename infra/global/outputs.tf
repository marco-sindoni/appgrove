output "zone_id" {
  description = "ID della hosted zone Route53 di appgrove.app."
  value       = aws_route53_zone.main.zone_id
}

output "name_servers" {
  description = "Name server della zona: vanno impostati sul registrar se il dominio non è registrato su Route53 (o se la zona è stata ricreata)."
  value       = aws_route53_zone.main.name_servers
}

output "edge_certificate_arns" {
  description = "ARN dei certificati ACM in us-east-1 (CloudFront), per livello prod/test."
  value       = { for k, c in aws_acm_certificate.edge : k => c.arn }
}

output "regional_certificate_arns" {
  description = "ARN dei certificati ACM in eu-west-1 (API Gateway), per livello prod/test."
  value       = { for k, c in aws_acm_certificate.regional : k => c.arn }
}

output "github_actions_role_arns" {
  description = "ARN dei ruoli OIDC della CI (da referenziare nei workflow, UC 0005)."
  value       = { for k, r in aws_iam_role.github_actions : k => r.arn }
}
