#!/bin/sh

backup_folder="/backup"
echo "getting redis pods"
pods=$(kubectl get pods -o=name --field-selector status.phase=Running | grep "redis-server" | sed "s/^.\{4\}//")
if [ $? -ne 0 ]; then
  echo "failed to get redis pods"
  exit 1
fi
echo "redis pods: $pods"
#for each pod, restore the dump file
for pod in $pods; do
  echo "copy dump file to $pod"
  kubectl exec $pod -- sh -c 'rm -rf /data/*.rdb'
  kubectl exec $pod -- sh -c 'rm -rf /data/appendonlydir'
  if [ $? -ne 0 ]; then
    echo "failed to remove data from $pod"
    exit 1
  fi
  kubectl cp $backup_folder/data $pod:/ &
  if [ $? -ne 0 ]; then
    echo "failed to copy data to $pod"
    exit 1
  fi
done
wait
for pod in $pods; do
  echo "finish copy to $pod"
  kubectl exec $pod -- sh -c 'ls -lrt /data'
  kubectl exec $pod -- sh -c 'ls -lrt /data/**'
  echo "delete $pod"
  kubectl delete --force --grace-period=0 pod $pod
done
