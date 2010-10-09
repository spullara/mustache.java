#!/usr/bin/env ruby

require 'rubygems'
require 'mustache'
require 'tmpdir'
require 'yaml'
require 'test/unit'

Dir[File.join(File.dirname(__FILE__), '..', 'specs', '*.yml')].each do |file|
  spec = YAML.load_file(file)

  Class.new(Test::Unit::TestCase) do
    define_method :name do
      File.basename(file).sub(/^./, &:upcase)
    end

    def setup
      @partials = File.join(File.dirname(__FILE__), '..', 'partials')
      Dir.mkdir(@partials)
      Mustache.template_path = @partials
    end

    def teardown
      Dir[File.join(@partials, '*')].each { |file| File.delete(file) }
      Dir.rmdir(@partials)
    end

    def write_partials(partials)
      partials.each do |name, content|
        File.open(File.join(@partials, "#{name}.mustache"), 'w') do |f|
          f.print(content)
        end
      end
    end

    spec['tests'].each do |test|
      define_method :"test: #{test['name']}" do
        write_partials test['partials'] if test.has_key? 'partials'

        result = Mustache.render(test['template'], test['data'])
        assert_equal test['expected'], result, <<-MSG.gsub(/^\s+/, '')
          Test: #{test['name']}
          Data: #{test['data'].inspect}
          Template: #{test['template'].inspect}
          Partials: #{(test['partials'] || {}).inspect}
        MSG
      end
    end
  end
end
