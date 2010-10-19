#!/usr/bin/env ruby

require 'rubygems'
require 'mustache'
require 'tmpdir'
require 'yaml'
require 'test/unit'

YAML::add_builtin_type('code') { |_, val| eval(val['ruby']) }

class MustacheSpec < Test::Unit::TestCase
  def setup
    @partials = File.join(File.dirname(__FILE__), '..', 'partials')
    Dir.mkdir(@partials)
    Mustache.template_path = @partials
  end

  def teardown
    Dir[File.join(@partials, '*')].each { |file| File.delete(file) }
    Dir.rmdir(@partials)
  end

  def setup_partials(test)
    (test['partials'] || {}).each do |name, content|
      File.open(File.join(@partials, "#{name}.mustache"), 'w') do |f|
        f.print(content)
      end
    end
  end

  def assert_mustache_spec(test)
    actual = Mustache.render(test['template'], test['data'])

    assert_equal test['expected'], actual, "" <<
      "Test: #{ test['name'] }\n" <<
      "Data: #{ test['data'].inspect }\n" <<
      "Template: #{ test['template'].inspect }\n" <<
      "Partials: #{ (test['partials'] || {}).inspect }\n"
  end

  def test_noop; assert(true); end
end

Dir[File.join(File.dirname(__FILE__), '..', 'specs', '*.yml')].each do |file|
  spec = YAML.load_file(file)

  Class.new(MustacheSpec) do
    define_method :name do
      File.basename(file).sub(/^./, &:upcase)
    end

    spec['tests'].each do |test|
      define_method :"test - #{test['name']}" do
        setup_partials(test)
        assert_mustache_spec(test)
      end
    end
  end
end
