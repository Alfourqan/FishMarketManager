{pkgs}: {
  deps = [
    pkgs.sqlite #base de donnees sqlite
    pkgs.postgresql #base de donnees SQL
    pkgs.maven  #outil de build pour java
  ];
}
