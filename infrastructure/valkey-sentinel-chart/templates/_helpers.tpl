{{/*
차트 fullname
*/}}
{{- define "valkey.fullname" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
공통 labels
*/}}
{{- define "valkey.labels" -}}
app.kubernetes.io/name: {{ include "valkey.fullname" . }}
app.kubernetes.io/part-of: {{ include "valkey.fullname" . }}-sentinel
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version }}
{{- end }}

{{/*
Data Node selector labels
*/}}
{{- define "valkey.data.selectorLabels" -}}
app.kubernetes.io/name: {{ include "valkey.fullname" . }}
app.kubernetes.io/component: data
{{- end }}

{{/*
Sentinel selector labels
*/}}
{{- define "valkey.sentinel.selectorLabels" -}}
app.kubernetes.io/name: {{ include "valkey.fullname" . }}
app.kubernetes.io/component: sentinel
{{- end }}

{{/*
Data Node Headless Service FQDN
*/}}
{{- define "valkey.headless.fqdn" -}}
{{ include "valkey.fullname" . }}-headless.{{ .Release.Namespace }}.svc.cluster.local
{{- end }}

{{/*
Sentinel Service FQDN
*/}}
{{- define "valkey.sentinel.fqdn" -}}
{{ include "valkey.fullname" . }}-sentinel.{{ .Release.Namespace }}.svc.cluster.local
{{- end }}

{{/*
초기 Master Pod FQDN (Pod-0)
*/}}
{{- define "valkey.initialMaster.fqdn" -}}
{{ include "valkey.fullname" . }}-0.{{ include "valkey.headless.fqdn" . }}
{{- end }}
