#!/bin/bash
awslocal s3 mb s3://lexai-documents

# Template files for DocumentExamples
# Path format: knowledge/templates/document-examples/{exampleId}/{fileName}.extension

# Private generation knowledge
awslocal s3 cp /tmp/knowledge/manifest.json s3://lexai-documents/knowledge/manifest.json
awslocal s3 sync /tmp/knowledge/conversations s3://lexai-documents/knowledge/conversations --exclude ".gitkeep" --exclude "**/.gitkeep"


# Team ID: 550e8400-e29b-41d4-a716-446655440100
# Case ID: 650e8400-e29b-41d4-a716-446655440001
# URL format: teamId/caseId/groupId/fileId

# Group a50e8400-e29b-41d4-a716-446655440001 (Contract Documents)
awslocal s3 cp /tmp/knowledge/samples/sample.pdf s3://lexai-documents/550e8400-e29b-41d4-a716-446655440100/650e8400-e29b-41d4-a716-446655440001/a50e8400-e29b-41d4-a716-446655440001/b50e8400-e29b-41d4-a716-446655440001.pdf
awslocal s3 cp /tmp/knowledge/samples/sample.jpg s3://lexai-documents/550e8400-e29b-41d4-a716-446655440100/650e8400-e29b-41d4-a716-446655440001/a50e8400-e29b-41d4-a716-446655440001/b50e8400-e29b-41d4-a716-446655440002.jpg
# Group a50e8400-e29b-41d4-a716-446655440002 (Court Filings)
awslocal s3 cp /tmp/knowledge/samples/sample.pdf s3://lexai-documents/550e8400-e29b-41d4-a716-446655440100/650e8400-e29b-41d4-a716-446655440001/a50e8400-e29b-41d4-a716-446655440002/b50e8400-e29b-41d4-a716-446655440003.pdf
# Group a50e8400-e29b-41d4-a716-446655440003 (Evidence Collection)
awslocal s3 cp /tmp/knowledge/samples/sample.jpg s3://lexai-documents/550e8400-e29b-41d4-a716-446655440100/650e8400-e29b-41d4-a716-446655440001/a50e8400-e29b-41d4-a716-446655440003/b50e8400-e29b-41d4-a716-446655440004.png
awslocal s3 cp /tmp/knowledge/samples/sample.txt s3://lexai-documents/550e8400-e29b-41d4-a716-446655440100/650e8400-e29b-41d4-a716-446655440001/a50e8400-e29b-41d4-a716-446655440003/b50e8400-e29b-41d4-a716-446655440005.docx
# Group a50e8400-e29b-41d4-a716-446655440004 (Case Pleadings)
awslocal s3 cp /tmp/knowledge/samples/sample.pdf s3://lexai-documents/550e8400-e29b-41d4-a716-446655440100/650e8400-e29b-41d4-a716-446655440001/a50e8400-e29b-41d4-a716-446655440004/b50e8400-e29b-41d4-a716-446655440006.pdf
awslocal s3 cp /tmp/knowledge/samples/sample.pdf s3://lexai-documents/550e8400-e29b-41d4-a716-446655440100/650e8400-e29b-41d4-a716-446655440001/a50e8400-e29b-41d4-a716-446655440004/b50e8400-e29b-41d4-a716-446655440007.pdf
# Group a50e8400-e29b-41d4-a716-446655440005 (Case Discovery)
awslocal s3 cp /tmp/knowledge/samples/sample.pdf s3://lexai-documents/550e8400-e29b-41d4-a716-446655440100/650e8400-e29b-41d4-a716-446655440001/a50e8400-e29b-41d4-a716-446655440005/b50e8400-e29b-41d4-a716-446655440008.pdf
awslocal s3 cp /tmp/knowledge/samples/sample.txt s3://lexai-documents/550e8400-e29b-41d4-a716-446655440100/650e8400-e29b-41d4-a716-446655440001/a50e8400-e29b-41d4-a716-446655440005/b50e8400-e29b-41d4-a716-446655440009.docx
# Group a50e8400-e29b-41d4-a716-446655440006 (Sample Files)
awslocal s3 cp /tmp/knowledge/samples/sample.pdf s3://lexai-documents/550e8400-e29b-41d4-a716-446655440100/650e8400-e29b-41d4-a716-446655440001/a50e8400-e29b-41d4-a716-446655440006/b50e8400-e29b-41d4-a716-446655440010.pdf
awslocal s3 cp /tmp/knowledge/samples/sample.jpg s3://lexai-documents/550e8400-e29b-41d4-a716-446655440100/650e8400-e29b-41d4-a716-446655440001/a50e8400-e29b-41d4-a716-446655440006/b50e8400-e29b-41d4-a716-446655440011.jpg
awslocal s3 cp /tmp/knowledge/samples/sample.txt s3://lexai-documents/550e8400-e29b-41d4-a716-446655440100/650e8400-e29b-41d4-a716-446655440001/a50e8400-e29b-41d4-a716-446655440006/b50e8400-e29b-41d4-a716-446655440012.txt
