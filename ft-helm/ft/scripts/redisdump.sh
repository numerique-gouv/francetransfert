#!/bin/sh
# get one redis pod
pod=`kubectl get pods -o=name --field-selector status.phase=Running | grep "redis-node" | sed "s/^.\{4\}//" | head -n 1`
echo "exec save command on redis $pod"
kubectl exec $pod -- redis-cli -a $METALOAD_PASSWORD save
echo "finish saving redis $pod"
echo "copy dump file from $pod to /backup/redis-dump"
kubectl cp $pod:/data /backup/
echo "finish copying dump file"
