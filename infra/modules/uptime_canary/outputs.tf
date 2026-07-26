output "sns_topic_arn" {
  description = "Topic SNS (eu-central-1) su cui il canary pubblica gli allarmi di uptime."
  value       = aws_sns_topic.alarms.arn
}

output "lambda_function_name" {
  description = "Nome della Lambda di ping."
  value       = aws_lambda_function.ping.function_name
}

output "alarm_name" {
  description = "Nome dell'allarme di uptime."
  value       = aws_cloudwatch_metric_alarm.uptime.alarm_name
}
