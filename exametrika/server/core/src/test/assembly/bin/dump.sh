#!/bin/sh
wget -O - --user Admin --password adminadmin --no-check-certificate --post-file dump.json https://localhost:8443/api/ops/dump
