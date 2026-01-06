#!/bin/bash
awslocal s3 mb s3://lexai-documents

# Team ID: 550e8400-e29b-41d4-a716-446655440100
# Case ID: 650e8400-e29b-41d4-a716-446655440001 (and 'default' for files without case)

# Default group files
awslocal s3 cp /tmp/sample-files/sample.pdf s3://lexai-documents/550e8400-e29b-41d4-a716-446655440100/default/b50e8400-e29b-41d4-a716-446655440001.pdf
awslocal s3 cp /tmp/sample-files/sample.jpg s3://lexai-documents/550e8400-e29b-41d4-a716-446655440100/default/b50e8400-e29b-41d4-a716-446655440002.jpg
awslocal s3 cp /tmp/sample-files/sample.pdf s3://lexai-documents/550e8400-e29b-41d4-a716-446655440100/default/b50e8400-e29b-41d4-a716-446655440003.pdf
awslocal s3 cp /tmp/sample-files/sample.jpg s3://lexai-documents/550e8400-e29b-41d4-a716-446655440100/default/b50e8400-e29b-41d4-a716-446655440004.png
awslocal s3 cp /tmp/sample-files/sample.txt s3://lexai-documents/550e8400-e29b-41d4-a716-446655440100/default/b50e8400-e29b-41d4-a716-446655440005.docx

# Case-related files
awslocal s3 cp /tmp/sample-files/sample.pdf s3://lexai-documents/550e8400-e29b-41d4-a716-446655440100/650e8400-e29b-41d4-a716-446655440001/b50e8400-e29b-41d4-a716-446655440006.pdf
awslocal s3 cp /tmp/sample-files/sample.pdf s3://lexai-documents/550e8400-e29b-41d4-a716-446655440100/650e8400-e29b-41d4-a716-446655440007.pdf
awslocal s3 cp /tmp/sample-files/sample.pdf s3://lexai-documents/550e8400-e29b-41d4-a716-446655440100/650e8400-e29b-41d4-a716-446655440008.pdf
awslocal s3 cp /tmp/sample-files/sample.txt s3://lexai-documents/550e8400-e29b-41d4-a716-446655440100/650e8400-e29b-41d4-a716-446655440009.docx
awslocal s3 cp /tmp/sample-files/sample.pdf s3://lexai-documents/550e8400-e29b-41d4-a716-446655440100/650e8400-e29b-41d4-a716-446655440010.pdf
awslocal s3 cp /tmp/sample-files/sample.jpg s3://lexai-documents/550e8400-e29b-41d4-a716-446655440100/650e8400-e29b-41d4-a716-446655440011.jpg
awslocal s3 cp /tmp/sample-files/sample.txt s3://lexai-documents/550e8400-e29b-41d4-a716-446655440100/650e8400-e29b-41d4-a716-446655440012.txt
