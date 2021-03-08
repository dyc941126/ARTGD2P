# Implementation of GD<sup>2</sup>P, ST-GD<sup>2</sup>P & PTS
## Problem generation
Please refer to
https://github.com/dyc941126/DCOPBenchmarks

## Run algorithms
Pass the following arguments to main class ``org.dyc.main.Solve``:
```bash
-p            path to problem file
-am am_file   path to agent_manifest.xml
-a algo       acceleration algorithm [GDP|GD2P|FDSP|ST-GD2P|PTS|HOP]
optional arguments
-c cycle      numer of cycles (iterations) to run (default=2000)
-t step_size  step size for ST-GD2P & PTS
-cr criterion sorting criterion for PTS [MAX|MEAN|QUANTILE|HINDEX]
-s depth      sorting depth for PTS
```
Example
```
java -cp ARTGD2P.jar org.dyc.main.BatchRun -p foo.xml -am path/to/agent_manifest.xml -a PTS -c 2000 -t 1 -cr MAX -s 3
```