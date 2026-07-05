variable "env" {
  description = "Nome dell'ambiente (test | prod); entra nei nomi delle risorse e nei path SSM."
  type        = string

  validation {
    condition     = contains(["test", "prod"], var.env)
    error_message = "env deve essere \"test\" o \"prod\" (local non usa AWS, #12 1/3)."
  }
}

variable "vpc_cidr" {
  description = "CIDR della VPC dell'ambiente (distinto tra test e prod per non precludere un futuro peering)."
  type        = string
}

variable "force_destroy_buckets" {
  description = <<-EOT
    Sicurezze di destroy (#06 24): true su test (teardown libero: i bucket si
    svuotano da soli), false su prod (i bucket vanno svuotati esplicitamente).
  EOT
  type        = bool
}
