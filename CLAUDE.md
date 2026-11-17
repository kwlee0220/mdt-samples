# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

`mdt-samples` is a collection of small runnable example programs for the MDT platform client API: instances, submodels, parameters, registries, time series, HTTP/AAS operations, robot-twin operations, and workflow models. Every class under `mdt.sample` has its own `main`. Nothing is a library and nothing is a service. There are no tests.

The project is not a git repository. Source comments are in Korean. Match that when editing near them. [README.md](README.md) is two lines.

## Build and run

Composite build: `settings.gradle` includes `../../common/utils` (`:utils`) and `../mdt-client` (`:mdt-client`). Library versions come from `../mdt-client/gradle/libs.versions.toml`. There is no Gradle wrapper: use the system `gradle`. The jar version is `$MDT_BUILD_VERSION`, or `unknown` when it is unset.

```bash
gradle compileJava                                  # the only automated check: there are no tests
gradle runSample -Psample=ListMDTInstances          # runs mdt.sample.<name>; stdin is forwarded
gradle runSample -Psample=rt.SamplePickNPlace       # sub-packages: prefix the class name
gradle runSample -Psample=SampleTimeSeriesBatchReader --args='2023-05-25T14:00:00Z 5'
gradle shadowJar                                    # fat jar (no Main-Class; run with java -cp ... mdt.sample.X)
```

- **`runSample`** resolves `-Psample` to `mdt.sample.<value>`. An unknown name fails with `ClassNotFoundException`. The working directory is this project directory.
- **"It compiles" proves little.** Most samples call a live MDT manager, instance, operation server or workflow manager, and their mistakes only show at runtime (see "Known problems" below).

## Connecting to MDT

- **Preferred:** `HttpMDTManager.connectWithDefault()`. It reads `mdt_client_config.yaml` from `$MDT_CLIENT_HOME`, or from the current directory when that is unset (this project directory under `runSample`). If the file is absent it falls back to the `MDT_URL` environment variable. If the file exists but cannot be read, it throws instead of falling back. Use this in new or edited samples.
- **Hard-coded endpoints exist** and should not be copied:
  - `HttpMDTManager.connect("http://localhost:12985")`.
  - Public or LAN IPs: `61.98.138.54`, `129.254.91.134`, `218.158.72.211`, `192.168.0.7`.
  - Absolute paths under `/home/kwlee/...`.
- **`SampleRemoveInstance` deletes** instance `Test` on the manager it points at. Several others modify live state:
  - `ListParametersOfInstance` overwrites parameter `Status` and then restores it.
  - `StartStopMDTInstance` leaves the instance stopped.
  - `SampleAddInstance` uploads instance bundles.
  - The workflow samples register (replace) workflow models.

## Layout (`src/main/java/mdt/sample`)

| Area | Classes | What they need at runtime |
|---|---|---|
| Instances | `ListMDTInstances`, `GetMDTInstance`, `ListMDTInstancesByFilter`, `GetInstancesByFilter`, `StartStopMDTInstance`, `SampleAddInstance`, `SampleRemoveInstance`, `Utils` (print helper) | instances `test` / `Test`, `HEAT-*` |
| Submodels / parameters | `ListSubmodelsOfInstance`, `GetSubmodelOfInstance`, `ListParametersOfInstance` | instances `Welder`, `test` |
| Registries | `SampleShellRegistry`, `SampleSubmodelRegistry` | manager on `localhost:12985` |
| Time series | `SampleTimeSeriesElementReference`, `SampleTimeSeriesRecordsReference` (near-duplicates; reference expressions like `timeseries:Welder:NozzleProductionLog#last=5`), `SampleTimeSeriesBatchReader` (fixed-size batches from a cursor file `timeseries-cursor.txt`; calls the `ProductionEstimation` AAS operation per batch) | instance `Welder` |
| HTTP operation | `SampleRunHttpOperation`: `RESTfulAsyncRpcClient` against mdt-operation-http | instance `test`, operation server on `localhost:12987` |
| AASX | `ParseAasxExample <file.aasx>`: aas4j `AASXDeserializer`, with a fallback that inserts missing `<value>` into `Key` elements | a local file only |
| Robot twin (`rt`) | `SampleRunMoveLinear`, `SampleMoveToJoints`, `SamplePickNPlace`: `AASOperationClient` on `Data` submodel operations `Operations.MoveLinear` / `MoveToJoints` / `MoveGripperToTarget` / `MoveToNamedTarget`, with mdt-client's `PoseSerDe` / `JointsSerDe` | instance `panda01` (see the Panda instance layout under `~/mdt/models/panda/`) |
| Workflows (`workflow`) | `SampleWorkflowDescriptor0`–`6`, `3_1`, `SampleTimeSeriesTask`, `WfProcessSimulation`, `WfProcessOptimization`, `WfThicknessDefectInspection`: build a `WorkflowModel` of `TaskDescriptor`s and call `WorkflowManager.addOrReplaceWorkflowModel` | workflow manager; instances `test`, `ktech_inspector`, `inspector`, `heater`/`former`/`trimmer`/`innercase` |

`src/main/resources/logback.xml` logs at INFO and silences `mdt.client.operation` below WARN so polling does not drown the output.

## Rules the samples must follow

These rules come from the current mdt-client / mdt-workflow / mdt-operation-http / mdtpy code. Check against them when writing or fixing a sample.

- **Workflow model ids, task ids and argument ids must be Python identifiers.** mdt-workflow renders each model into an Airflow DAG and rejects others (`invalid DAG id ... (invalid Python identifier)`; `OperatorSpec` and `DagArgument` check task and argument ids the same way). Use `sample_workflow_0` and `copy_data`, not `sample-workflow-0` and `copy-data`.
- **`SetTask` arguments are `source` (input) and `target` (output)** (`SetTask.ARG_SOURCE` / `ARG_TARGET`). mdtpy's `SetElementOperator` raises "Input argument 'source' is required" otherwise.
- **`RESTfulAsyncRpcTask` options are `baseUrl` and `endpoint`, both required** (`RESTfulAsyncRpcTask.OPTION_BASE_URL` / `OPTION_OPERATION_ENDPOINT`). The `server` / `id` options are not read.
- **Where `/api/v1` goes:** `baseUrl` is the operation server root (`http://host:12987`) and `endpoint` carries `/api/v1/operations/<opId>`. The client appends both `endpoint` and the returned session endpoint (`/api/v1/sessions/...`) to `baseUrl`. So a `baseUrl` ending in `/api/v1` breaks polling, and an `endpoint` without `/api/v1` gives a 404 on start.
- **AAS operations:** use `AASOperationTask` with `TaskDescriptor.OPTION_OPERATION` (an element reference expression to the `Operation` element).
- **Timeouts and poll intervals** take duration strings (`"1m"`, `"3s"`) or plain numbers meaning seconds. In Airflow, `timeout` / `poll` are ignored with a warning for most operators. A typo therefore surfaces only when the task is run as a Java `MDTTask`.
- **Operation ids on mdt-operation-http** are the directory names under `$MDT_HOME/mdt-operation-http/operations/` (e.g. `AddAndSleep`, `CountRecords`, `ProcessSimulation`, `ThicknessInspection`, `UpdateDefectList`).
- **`Execution.waitForFinished(timeout, unit)` throws `TimeoutException`** when the execution is still running. It does not return a running result, so check for it with `catch`, not with `isRunning()`.
- **Robot operation outputs:** look them up by idShort rather than `getOutputArguments().getFirst()`.

## Known problems

- **Workflow samples break the rules above.** Every workflow sample except `SampleTimeSeriesTask` uses hyphenated ids, so registration fails.
  - Seven samples pass `SetTask` an input named `value`: `SampleWorkflowDescriptor1`/`2`/`5`/`6` and the three `Wf*` classes.
  - `SampleWorkflowDescriptor5`/`6` and the `Wf*` classes use the `server` / `id` options, which are not read.
  - `SampleTimeSeriesTask` and `SampleWorkflowDescriptor3` put `/api/v1` in the wrong place.
  - `SampleWorkflowDescriptor3_1` pairs `RESTfulAsyncRpcTask` with `OPTION_OPERATION` and adds its output as an input.
  - `SampleWorkflowDescriptor4` has timeout `"im"`.
  - `SampleWorkflowDescriptor6`'s `copy-lt-error` copies the short-term (`STErrorPrediction`) output.
  - `WfProcessSimulation` has the reference `param:inspector0` (missing `:`).
- **`SampleRunHttpOperation`:** it uses operation id `Simulation`, which is not deployed; its inputs match `AddAndSleep`. Its cancel branch after `waitForFinished(5s)` is unreachable.
- **`SampleTimeSeriesBatchReader`:** it casts operation outputs with `(Double)` / `(String)` and reads the ampere field by index.
- **Stale comments:** some comments disagree with the code (instance `'test'` vs `Welder`; the pick-and-place waypoint table vs the constants).

Ask before fixing them in bulk.
