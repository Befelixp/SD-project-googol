#!/bin/bash

# Verificar se o processo do RMIGateway está em execução
pid=$(ps aux | grep 'meta1sd.RMIGateway' | grep -v 'grep' | awk '{print $2}')

if [ -z "$pid" ]; then
    echo "RMI Gateway is not running."
else
    echo "Stopping RMI Gateway with PID: $pid"
    kill $pid
    echo "RMI Gateway stopped."
fi