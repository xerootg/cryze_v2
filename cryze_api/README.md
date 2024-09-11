# Cryze API

## Views
 - Cameras - for configuring the relationship between camera IDs and the name of the stream
 - Settings - for viewing/updating credentials
  - Note: If there are no credentials set, it attempts to load them from the environment. By default, it will do this at startup and overwrite any channges you make. this can be turned off by setting <figure out the IConfiguration env var for this>=false
 - Camera Messages - view any messages the SDK has sent us about each camera

## Views - TODO
 - Cameras - Sync from your wyze account
 - App controls - view the android app, force quit, start, stop, reload, see logs, etc

## pythonnet
 - does `wyze_sdk` things. probably fragile :(
