variable "env" {
  description = "Nome dell'ambiente (test | prod); entra nei nomi delle risorse e nei domini."
  type        = string

  validation {
    condition     = contains(["test", "prod"], var.env)
    error_message = "env deve essere \"test\" o \"prod\" (local non usa AWS, #12 1/3)."
  }
}

variable "domain" {
  description = "Dominio unico dell'iniziativa (#12 12); test vive sul sottodominio test.<domain> (#12 10)."
  type        = string
  default     = "appgrove.app"
}

variable "vpc_id" {
  description = "VPC dell'ambiente (output di env_baseline)."
  type        = string
}

variable "vpc_cidr" {
  description = "CIDR della VPC: perimetro delle regole dei security group interni."
  type        = string
}

variable "subnet_ids" {
  description = "Subnet dell'ambiente (2 AZ, output di env_baseline) per Aurora/Proxy/VPC Link."
  type        = list(string)
}

variable "deletion_protection" {
  description = <<-EOT
    Sicurezze di destroy sul cluster Aurora (#06 16/24): true su prod
    (deletion protection + snapshot finale), false su test (destroy libero).
  EOT
  type        = bool
}

variable "force_destroy_buckets" {
  description = "Come in env_baseline (#06 24): true su test (i bucket SPA si svuotano da soli), false su prod."
  type        = bool
}

variable "use_fargate_spot" {
  description = "Capacità ECS di default (#06 10): Fargate Spot in test (~-70%), on-demand in prod."
  type        = bool
}

variable "alert_email" {
  description = "Destinatario email degli allarmi (#08 15): sottoscritto ai topic SNS critical/warning (la subscription va confermata via mail)."
  type        = string
}

variable "auth_lambda_s3_key" {
  description = <<-EOT
    Chiave S3 (nel bucket artefatti per-env) del function.zip della Lambda BFF
    auth, per-SHA come le immagini ECR (es. auth/<sha>-native.zip). La imposta
    la CI via TF_VAR_auth_lambda_s3_key (UC 0005/0015). Vuota finché la prima
    build non è pubblicata: Lambda e route /api/auth/* non vengono create
    (attivazione a fasi, _BACKLOG).
  EOT
  type        = string
  default     = ""
}
