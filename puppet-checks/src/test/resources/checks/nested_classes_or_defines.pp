class apache {
  class ssl { } # non compliant
}

# or

class apache {
  define config() { } # non compliant
}

class without {

}

class bar {
  class { 'foo':
    bar => 'foobar'
  }
}
