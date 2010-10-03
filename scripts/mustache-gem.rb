#!/usr/bin/env ruby

require 'rubygems'
require 'mustache'
require 'tmpdir'
require 'yaml'

begin
  require 'Win32/Console/ANSI' if RUBY_PLATFORM =~ /win32/
rescue LoadError
  raise 'You must gem install win32console to use color on Windows'
end

def colorize(text, color)
  return "\e[#{color}m#{text}\e[0m"
end

def partials(*file)
  return File.join(File.dirname(__FILE__), 'partials', *file)
end
alias :partial :partials

Mustache.template_path = partials

spec = YAML.load_file(File.join(File.dirname(__FILE__), '..', 'spec.yml'))
puts "Testing compliance with v#{ spec['version'] }\n"

failures = spec['tests'].reject do |test|
  begin
    print "Testing #{ test['name'].downcase }... "

    if test.has_key?('partials')
      Dir.mkdir(partials)
      test['partials'].each do |file, content|
        File.open(partial("#{file}.mustache"), 'w') { |f| f.print(content) }
      end
    end

    test['output'] = Mustache.render(test['template'], test['data'])
    if test['output'] == test['expected']
      print colorize("PASSED\n", 32)
      true
    else
      puts colorize("FAILED!", 31)

      puts "Given the template:", '', colorize(test['template'], '4;33')
      if test.has_key?('partials')
        puts "Partials:", ''
        test['partials'].each do |file, content|
          puts colorize("#{file}.mustache", 43)
          puts colorize(content, '4;33')
        end
      end
      puts "And data:", '',           test['data'].inspect, ''
      puts "Mustache rendered:", '',  colorize(test['output'], '4;31')
      puts "Instead of:", '',         colorize(test['expected'], '4;32')
      false
    end
  ensure
    Dir[File.join(partials, '*')].each { |file| File.delete(file) }
    Dir.rmdir(partials) if File.exists?(partials)
  end
end

puts
puts colorize("Final Results: #{failures.length} failures", 0)
failures.each { |e| puts "Failed #{e['name']}" }
