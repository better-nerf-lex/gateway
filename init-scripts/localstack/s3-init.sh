#!/bin/bash
awslocal s3 mb s3://lexai-documents
awslocal s3 cp /tmp/sample-files/sample.txt s3://lexai-documents/samples/sample.txt
awslocal s3 cp /tmp/sample-files/sample.pdf s3://lexai-documents/samples/sample.pdf
awslocal s3 cp /tmp/sample-files/sample.jpg s3://lexai-documents/samples/sample.jpg

# Seed case-001 files
awslocal s3 cp /tmp/sample-files/sample.pdf s3://lexai-documents/case-001/pleadings/complaint_filing.pdf
awslocal s3 cp /tmp/sample-files/sample.pdf s3://lexai-documents/case-001/pleadings/motion_to_dismiss.pdf
awslocal s3 cp /tmp/sample-files/sample.pdf s3://lexai-documents/case-001/discovery/deposition_transcript.pdf
awslocal s3 cp /tmp/sample-files/sample.txt s3://lexai-documents/case-001/discovery/interrogatories_response.docx

# Seed other sample directories if needed
awslocal s3 cp /tmp/sample-files/sample.pdf s3://lexai-documents/contracts/contract_scan_001.pdf
awslocal s3 cp /tmp/sample-files/sample.jpg s3://lexai-documents/contracts/signature_page.jpg
awslocal s3 cp /tmp/sample-files/sample.pdf s3://lexai-documents/filings/court_filing_2024.pdf
awslocal s3 cp /tmp/sample-files/sample.jpg s3://lexai-documents/evidence/evidence_photo_01.png
awslocal s3 cp /tmp/sample-files/sample.txt s3://lexai-documents/evidence/witness_statement.docx
