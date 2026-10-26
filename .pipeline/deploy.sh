#!/bin/sh
# Installs or upgrades a helm release with a chart in .pipeline/helm.
#
# Runs in the deploy steps of .woodpecker.yaml, inside the cluster. Everything
# that is specific to the cluster (namespace, registry, hosts, volumes, node
# selector, environment of the containers) is not part of this repository. The
# cluster provides it in the secret woodpecker-<release>-deploy, with the keys
# "namespace" and "values.yaml".
#
#   RELEASE_NAME  name of the helm release
#   CHART         chart in .pipeline/helm (default: RELEASE_NAME)
#   IMAGE_TAG     tag of the image that the pipeline built (default: the tag of the chart)
set -eu

release=${RELEASE_NAME:?RELEASE_NAME is required.}
chart="$(dirname "$0")/helm/${CHART:-$release}"
settings_secret="woodpecker-$release-deploy"
# The secret is in the namespace that the pipeline steps run in
pipeline_namespace=$(cat /var/run/secrets/kubernetes.io/serviceaccount/namespace)

setting() {
    kubectl get secret "$settings_secret" --namespace "$pipeline_namespace" \
        -o "go-template={{index .data \"$1\" | base64decode}}"
}

if ! namespace=$(setting namespace); then
    echo "The deploy settings of $release could not be read from the secret $settings_secret." >&2
    exit 1
fi

values_file=$(mktemp)
trap 'rm -f "$values_file"' EXIT
setting values.yaml > "$values_file"

set -- --namespace "$namespace" -f "$values_file"
if [ -n "${IMAGE_TAG:-}" ]; then
    set -- "$@" --set-string "image.tag=$IMAGE_TAG"
fi

report_runtime_versions() {
    label=$1
    echo "=== $label ==="
    kubectl version --output=yaml

    pod=$(kubectl get pods --namespace "$namespace" \
        --selector="app.kubernetes.io/instance=$release" \
        --field-selector=status.phase=Running \
        -o jsonpath='{.items[0].metadata.name}')
    if [ -z "$pod" ]; then
        echo "No running pod found for $release in $namespace."
        return 0
    fi

    kubectl exec --namespace "$namespace" "$pod" -- python -c \
        'import importlib.metadata as m, sys; print("Python==" + sys.version.split()[0]); print("\n".join(sorted(d.metadata["Name"] + "==" + d.version for d in m.distributions() if d.metadata.get("Name"))))'
}

if [ "${REPORT_RUNTIME_VERSIONS:-}" = "1" ]; then
    report_runtime_versions "busyhumans runtime before deployment"
fi

helm upgrade "$release" "$chart" --install --wait --timeout 10m "$@"

if [ "${REPORT_RUNTIME_VERSIONS:-}" = "1" ]; then
    report_runtime_versions "busyhumans runtime after deployment"
fi
