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

helm upgrade "$release" "$chart" --install --wait --timeout 10m "$@"
