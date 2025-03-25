#!/bin/sh

backup_folder="/backup"
echo "getting redis pods"
pods=$(kubectl get pods -o=name --field-selector status.phase=Running | grep "redis-node" | sed "s/^.\{4\}//")
if [ $? -ne 0 ]; then
  echo "failed to get redis pods"
  exit 1
fi
echo "redis pods: $pods"
#for each pod, restore the dump file
for pod in $pods; do
  echo "copy dump file to $pod"
  kubectl exec $pod -- rm -rf '/data/*'
  if [ $? -ne 0 ]; then
    echo "failed to remove data from $pod"
    exit 1
  fi
  kubectl cp $backup_folder/data $pod:/
  if [ $? -ne 0 ]; then
    echo "failed to copy data to $pod"
    exit 1
  fi
  echo `kubectl exec $pod -- ls -la /data`
  echo "finish copy to $pod"
  kubectl delete pod $pod
done
