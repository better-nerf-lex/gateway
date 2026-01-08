#!/bin/bash
awslocal s3 mb s3://lexai-documents

# Template files for DocumentExamples
# Path format: templates/document-examples/{exampleId}/{fileId}.extension
awslocal s3 cp /tmp/sample-files/sample.pdf s3://lexai-documents/templates/document-examples/11111111-1111-1111-1111-111111111111/t1111111-1111-1111-1111-111111111111.pdf
awslocal s3 cp /tmp/sample-files/sample.pdf s3://lexai-documents/templates/document-examples/11111111-1111-1111-1111-111111111111/t1111111-1111-1111-1111-111111111112.pdf
awslocal s3 cp /tmp/sample-files/sample.pdf s3://lexai-documents/templates/document-examples/33333333-3333-3333-3333-333333333333/t3333333-3333-3333-3333-333333333331.pdf
awslocal s3 cp /tmp/sample-files/sample.pdf s3://lexai-documents/templates/document-examples/33333333-3333-3333-3333-333333333333/t3333333-3333-3333-3333-333333333332.pdf
awslocal s3 cp /tmp/sample-files/sample.pdf s3://lexai-documents/templates/document-examples/44444444-4444-4444-4444-444444444444/t4444444-4444-4444-4444-444444444441.pdf
awslocal s3 cp /tmp/sample-files/sample.pdf s3://lexai-documents/templates/document-examples/44444444-4444-4444-4444-444444444444/t4444444-4444-4444-4444-444444444442.pdf
awslocal s3 cp /tmp/sample-files/sample.pdf s3://lexai-documents/templates/document-examples/55555555-5555-5555-5555-555555555555/t5555555-5555-5555-5555-555555555551.pdf
awslocal s3 cp /tmp/sample-files/sample.pdf s3://lexai-documents/templates/document-examples/55555555-5555-5555-5555-555555555555/t5555555-5555-5555-5555-555555555552.pdf
awslocal s3 cp /tmp/sample-files/sample.pdf s3://lexai-documents/templates/document-examples/66666666-6666-6666-6666-666666666666/t6666666-6666-6666-6666-666666666661.pdf
awslocal s3 cp /tmp/sample-files/sample.pdf s3://lexai-documents/templates/document-examples/66666666-6666-6666-6666-666666666666/t6666666-6666-6666-6666-666666666662.pdf

# Team ID: 550e8400-e29b-41d4-a716-446655440100
# Case ID: 650e8400-e29b-41d4-a716-446655440001
# URL format: teamId/caseId/groupId/fileId

# Group a50e8400-e29b-41d4-a716-446655440001 (Contract Documents)
awslocal s3 cp /tmp/sample-files/sample.pdf s3://lexai-documents/550e8400-e29b-41d4-a716-446655440100/650e8400-e29b-41d4-a716-446655440001/a50e8400-e29b-41d4-a716-446655440001/b50e8400-e29b-41d4-a716-446655440001.pdf
awslocal s3 cp /tmp/sample-files/sample.jpg s3://lexai-documents/550e8400-e29b-41d4-a716-446655440100/650e8400-e29b-41d4-a716-446655440001/a50e8400-e29b-41d4-a716-446655440001/b50e8400-e29b-41d4-a716-446655440002.jpg
# Group a50e8400-e29b-41d4-a716-446655440002 (Court Filings)
awslocal s3 cp /tmp/sample-files/sample.pdf s3://lexai-documents/550e8400-e29b-41d4-a716-446655440100/650e8400-e29b-41d4-a716-446655440001/a50e8400-e29b-41d4-a716-446655440002/b50e8400-e29b-41d4-a716-446655440003.pdf
# Group a50e8400-e29b-41d4-a716-446655440003 (Evidence Collection)
awslocal s3 cp /tmp/sample-files/sample.jpg s3://lexai-documents/550e8400-e29b-41d4-a716-446655440100/650e8400-e29b-41d4-a716-446655440001/a50e8400-e29b-41d4-a716-446655440003/b50e8400-e29b-41d4-a716-446655440004.png
awslocal s3 cp /tmp/sample-files/sample.txt s3://lexai-documents/550e8400-e29b-41d4-a716-446655440100/650e8400-e29b-41d4-a716-446655440001/a50e8400-e29b-41d4-a716-446655440003/b50e8400-e29b-41d4-a716-446655440005.docx
# Group a50e8400-e29b-41d4-a716-446655440004 (Case Pleadings)
awslocal s3 cp /tmp/sample-files/sample.pdf s3://lexai-documents/550e8400-e29b-41d4-a716-446655440100/650e8400-e29b-41d4-a716-446655440001/a50e8400-e29b-41d4-a716-446655440004/b50e8400-e29b-41d4-a716-446655440006.pdf
awslocal s3 cp /tmp/sample-files/sample.pdf s3://lexai-documents/550e8400-e29b-41d4-a716-446655440100/650e8400-e29b-41d4-a716-446655440001/a50e8400-e29b-41d4-a716-446655440004/b50e8400-e29b-41d4-a716-446655440007.pdf
# Group a50e8400-e29b-41d4-a716-446655440005 (Case Discovery)
awslocal s3 cp /tmp/sample-files/sample.pdf s3://lexai-documents/550e8400-e29b-41d4-a716-446655440100/650e8400-e29b-41d4-a716-446655440001/a50e8400-e29b-41d4-a716-446655440005/b50e8400-e29b-41d4-a716-446655440008.pdf
awslocal s3 cp /tmp/sample-files/sample.txt s3://lexai-documents/550e8400-e29b-41d4-a716-446655440100/650e8400-e29b-41d4-a716-446655440001/a50e8400-e29b-41d4-a716-446655440005/b50e8400-e29b-41d4-a716-446655440009.docx
# Group a50e8400-e29b-41d4-a716-446655440006 (Sample Files)
awslocal s3 cp /tmp/sample-files/sample.pdf s3://lexai-documents/550e8400-e29b-41d4-a716-446655440100/650e8400-e29b-41d4-a716-446655440001/a50e8400-e29b-41d4-a716-446655440006/b50e8400-e29b-41d4-a716-446655440010.pdf
awslocal s3 cp /tmp/sample-files/sample.jpg s3://lexai-documents/550e8400-e29b-41d4-a716-446655440100/650e8400-e29b-41d4-a716-446655440001/a50e8400-e29b-41d4-a716-446655440006/b50e8400-e29b-41d4-a716-446655440011.jpg
awslocal s3 cp /tmp/sample-files/sample.txt s3://lexai-documents/550e8400-e29b-41d4-a716-446655440100/650e8400-e29b-41d4-a716-446655440001/a50e8400-e29b-41d4-a716-446655440006/b50e8400-e29b-41d4-a716-446655440012.txt
