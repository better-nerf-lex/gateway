#!/bin/bash
awslocal s3 mb s3://lexai-documents

# Template files for DocumentExamples
# Path format: templates/document-examples/{exampleId}/{fileId}.extension
awslocal s3 cp /tmp/sample-files/pozew_rozwod_alimenty.pdf s3://lexai-documents/templates/document-examples/11111111-1111-1111-1111-111111111111/pozew_rozwod_alimenty.pdf
awslocal s3 cp /tmp/sample-files/pozew_rozwod_bez-dzieci.pdf s3://lexai-documents/templates/document-examples/22222222-2222-2222-2222-222222222222/pozew_rozwod_bez-dzieci.pdf


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
