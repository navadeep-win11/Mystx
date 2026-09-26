with open("/teamspace/studios/this_studio/Mystx/.github/workflows/build.yml", "r") as f:
    content = f.read()

content = content.replace(
    'PATCH=$(git tag -l "v${BASE_VERSION}.*" | grep -v beta || true | sed "s/v${BASE_VERSION}\.//" | sort -n | tail -1)',
    'PATCH=$(git tag -l "v${BASE_VERSION}.*" | (grep -v beta || true) | sed "s/v${BASE_VERSION}\\.//" | sort -n | tail -1)'
)
content = content.replace(
    'PATCH=$(git tag -l "v${BASE_VERSION}.*" | grep -v beta | sed "s/v${BASE_VERSION}\.//" | sort -n | tail -1)',
    'PATCH=$(git tag -l "v${BASE_VERSION}.*" | (grep -v beta || true) | sed "s/v${BASE_VERSION}\\.//" | sort -n | tail -1)'
)

with open("/teamspace/studios/this_studio/Mystx/.github/workflows/build.yml", "w") as f:
    f.write(content)
