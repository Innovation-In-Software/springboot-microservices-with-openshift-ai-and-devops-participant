# Classroom Cosign stand-in. Real clusters run: cosign verify <image>
# This script records that you reviewed the signature requirement.

param(
    [string]$Image = "md287/account-service:1.0.0"
)

Write-Host "SIGNATURE VERIFY (classroom)"
Write-Host "Image: $Image"
Write-Host "In production you would run:"
Write-Host "  cosign verify --key cosign.pub $Image"
Write-Host "Unsigned images must not be admitted to the namespace."
Write-Host "SIGNATURE: CLASSROOM-REVIEWED (not a cryptographic signature)"
exit 0
