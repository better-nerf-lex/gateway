#!/bin/bash
awslocal s3 mb s3://lexai-documents
awslocal s3 cp /tmp/sample-files/sample.txt s3://lexai-documents/samples/sample.txt
awslocal s3 cp /tmp/sample-files/sample.pdf s3://lexai-documents/samples/sample.pdf
awslocal s3 cp /tmp/sample-files/sample.jpg s3://lexai-documents/samples/sample.jpg
