# Fixture dei terraform test: DUE istanze di microsaas_app nello stesso
# ambiente, per verificare che le risorse siano disgiunte per app_id
# (rimuoverne una non tocca l'altra — safety di service-remove, #07 19).

terraform {
  required_version = ">= 1.9"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.7"
    }
  }
}

locals {
  shared = {
    vpc_id                        = "vpc-00000000"
    vpc_cidr                      = "10.0.0.0/16"
    subnet_ids                    = ["subnet-0000000a", "subnet-0000000b"]
    ecs_cluster_arn               = "arn:aws:ecs:eu-west-1:123456789012:cluster/appgrove-test"
    cloud_map_namespace_id        = "ns-0000000000000000"
    api_id                        = "api00000"
    vpc_link_id                   = "vl-0000"
    vpc_link_security_group_id    = "sg-00000000"
    event_bus_name                = "appgrove-test"
    event_bus_arn                 = "arn:aws:events:eu-west-1:123456789012:event-bus/appgrove-test"
    aurora_endpoint               = "appgrove-test.cluster-x.eu-west-1.rds.amazonaws.com"
    aurora_port                   = 5432
    aurora_database_name          = "appgrove"
    db_bootstrap_lambda_name      = "appgrove-test-db-bootstrap"
    sqs_queue_prefix              = "appgrove-test-"
    gdpr_export_results_queue_arn = "arn:aws:sqs:eu-west-1:123456789012:appgrove-test-gdpr-export-results"
    gdpr_export_bucket            = "appgrove-test-gdpr-export"
    gdpr_export_bucket_arn        = "arn:aws:s3:::appgrove-test-gdpr-export"
    alarm_topic_critical_arn      = "arn:aws:sns:eu-west-1:123456789012:appgrove-test-alarms-critical"
    alarm_topic_warning_arn       = "arn:aws:sns:eu-west-1:123456789012:appgrove-test-alarms-warning"
    audit_firehose_arn            = "arn:aws:firehose:eu-west-1:123456789012:deliverystream/appgrove-test-audit-archive"
    logs_to_firehose_role_arn     = "arn:aws:iam::123456789012:role/appgrove-test-logs-to-firehose"
    cognito_issuer                = "https://cognito-idp.eu-west-1.amazonaws.com/eu-west-1_test"
    cognito_jwks_url              = "https://cognito-idp.eu-west-1.amazonaws.com/eu-west-1_test/.well-known/jwks.json"
    cognito_client_id             = "test-client-id"
  }
}

module "alpha" {
  source = "../../.."

  env              = "test"
  app_id           = "alpha"
  use_fargate_spot = true
  force_destroy    = true
  shared           = local.shared
}

module "beta" {
  source = "../../.."

  env              = "test"
  app_id           = "beta"
  container_port   = 8081
  use_fargate_spot = true
  force_destroy    = true
  shared           = local.shared
}

output "alpha_export_queue" {
  value = module.alpha.gdpr_export_queue_name
}

output "beta_export_queue" {
  value = module.beta.gdpr_export_queue_name
}

output "alpha_schema" {
  value = module.alpha.db_schema
}

output "beta_schema" {
  value = module.beta.db_schema
}
