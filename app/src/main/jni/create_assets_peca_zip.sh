#!/bin/bash

#
# ui/html  -> peca.zip
#
set -e

peca_zip=$PWD/../assets/peca.zip

cd PeerCast-IM4U/ui/
zip $peca_zip -1 -r -X html

