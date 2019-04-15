import re , os, os.path
import json, io
import xml.sax.saxutils

def convertR(js, out):
  f = open(js, encoding="utf8")
  s = f.read()
  s = re.sub(r"(?m)^\s*//.*$", "", s)
  js = json.load(io.StringIO(s))

  os.makedirs(os.path.dirname(out), exist_ok=True)

  outf = open(out, "w", encoding="utf8")
  print("""<?xml version="1.0" encoding="utf-8"?>
<resources>
""", file=outf)

  for k, v in js.items(): 
    if not k or not v:
      continue

    k = k.lower()
    k = re.sub("[^a-z\d]", "_", k)
    k = "yt_" + k.rstrip("_")
    print(k, v)
    v = xml.sax.saxutils.escape(v)
    print('<string name="%s">%s</string>' % (k, v), file=outf)
    
  print(""" 
</resources>
""", file=outf)



def main():
  convertR("peercast-yt/ui/catalogs/ja.json", "values/yt.xml")
  convertR("peercast-yt/ui/catalogs/en.json", "values-en/yt.xml")
  convertR("peercast-yt/ui/catalogs/de.json", "values-de/yt.xml")
  convertR("peercast-yt/ui/catalogs/fr.json", "values-fr/yt.xml") 

if __name__ == "__main__":
  main()


