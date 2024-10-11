#!/bin/bash

az storage queue create -n pagopa-wallet-expiration-queue --connection-string='DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;QueueEndpoint=http://storage:10001/devstoreaccount1'
az storage queue create -n pagopa-wallet-cdc-queue --connection-string='DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;QueueEndpoint=http://storage:10001/devstoreaccount1'


# Define variables
QUEUE_NAME="pagopa-wallet-cdc-queue"
CONNECTION_STRING="DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;QueueEndpoint=http://storage:10001/devstoreaccount1"
TIMEOUT_DURATION=20
SLEEP_INTERVAL=5

# Function to check the queue for messages
check_queue() {
  az storage message get -q "$QUEUE_NAME" --connection-string="$CONNECTION_STRING" | grep -c PAGOPA
}

# Function to wait for messages in the queue
wait_for_messages() {
  local timeout=$1
  local start_time=$(date +%s)

  while [[ $(check_queue) -eq 0 ]]; do
    sleep "$SLEEP_INTERVAL"
    local current_time=$(date +%s)
    if (( current_time - start_time >= timeout )); then
      echo "Timeout reached, no messages found."
      return 1
    fi
  done
  echo "Message found in the queue."
  return 0
}

# Main script execution
timeout "$TIMEOUT_DURATION" bash -c "wait_for_messages $TIMEOUT_DURATION" || exit 1