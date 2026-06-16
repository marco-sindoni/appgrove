# Evoluzioni DevOps / infra (documento vivo)

Registro delle scelte fatte in versione **cost-min / semplice** ora, con il **percorso di hardening/scaling**
da attivare quando i requisiti lo giustificano. Aggiornato a ogni decisione rilevante. Ogni voce: stato attuale →
evoluzione, **trigger** (quando farlo), costo/impatto.

| # | Tema | Stato attuale (PoC) | Evoluzione | Trigger | Costo evoluzione |
|---|---|---|---|---|---|
| E1 | **Uscita rete servizi (NAT)** | Subnet **pubbliche** + security group stretti, **niente NAT** (#06 B) | Subnet **private** + **NAT Gateway** | Requisiti di isolamento/compliance, o tasks che non devono avere IP pubblico | ~$32/mese/ambiente |
| E2 | **Ingress API→container** | **VPC Link + Cloud Map** (service discovery), no ALB (#06 B) | **Application Load Balancer** | Traffico in crescita, health check avanzati, routing/sticky, WAF su ALB | ~$16/mese/ambiente + LCU |
| E3 | **Alta disponibilità Fargate** | **1 task** per servizio (no HA) | ≥2 task multi-AZ + autoscaling | Carico reale / SLA di uptime | costo ∝ n. task |
| E4 | **Aurora prod** | **scale-to-0 anche su prod** (#06 E) | min ACU >0 su prod per latenza costante | Cold-start del DB inaccettabile in prod | da ~$44/mese (0.5 ACU on) |
| E5 | **Ambienti effimeri PR** | Solo local/test/prod (#12) | Env effimero per PR in CI | Team più grande / molte PR parallele | variabile |
| E6 | **WAF** | Nessun WAF (#06 F) | AWS WAF su CloudFront/API GW | Esposizione pubblica con traffico reale / requisiti di sicurezza | ~$5–10/mese + per-regola/richiesta |
| E7 | **Cognito Advanced Security** | Lockout built-in + throttling API GW (usecases/01) | Cognito Advanced Security (adaptive, credenziali compromesse) | Traffico reale / requisiti anti-frode | a pagamento per MAU |

> Nota: tutte queste evoluzioni sono **modifiche Terraform**, non refactor applicativi (principio di isolamento incrementale).
