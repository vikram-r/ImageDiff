# ImageDiff

A tool written in Scala to find differences between 2 images. ImageDiff partitions the image into smaller pieces, then uses Akka 
and the Actor Model to concurrently "diff" each subimage in parallel. A resulting "diffed" image is created for each partition,
then sent back to be reassembled once all child Actors are finished processing. The final diffed image, containing any changed 
pixels on a white background, is saved into a file.


# Usage

ImageDiff is built with `sbt`, so first install the CLI.

From the project directory, run:

```sbt "run <original> <new>"```

where `<original>` and `<new>` should be replaced by the relative path to the images you want to diff

After execution, the resulting diffed image will be saved into a file named `diffed`

