#!/bin/bash

az storage queue create -n pagopa-wallet-expiration-queue --connection-string='DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;QueueEndpoint=http://storage:10001/devstoreaccount1'
az storage queue create -n pagopa-wallet-cdc-queue --connection-string='DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;QueueEndpoint=http://storage:10001/devstoreaccount1'

timeout 20 bash -c "while [[ $(az storage message get -q pagopa-wallet-cdc-queue  --connection-string='DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;QueueEndpoint=http://storage:10001/devstoreaccount1' | grep -c PAGOPA) -eq "0" ]]; do sleep 5; done" || exit 1
