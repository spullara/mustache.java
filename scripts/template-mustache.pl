#!/usr/bin/env perl

use Test::Mini;

use strict;
use warnings;
use Test::Mini::Assertions;

use Data::Dumper;
$Data::Dumper::Terse = 1;
$Data::Dumper::Useqq = 1;
$Data::Dumper::Quotekeys = 0;
$Data::Dumper::Indent = 0;
$Data::Dumper::Sortkeys = 1;
$Data::Dumper::Deparse = 1;

use File::Basename        qw/ basename dirname /;
use File::Spec::Functions qw/ catdir catfile /;
use Template::Mustache;
use YAML::XS ();

{
    # YAML::XS will automatically bless lambdas as a new instance of this.
    package code;
    use overload '&{}' => sub { eval shift->{perl} }
}

{
    package MustacheSpec;
    use base 'Test::Mini::TestCase';

    use File::Path            qw/ rmtree /;
    use File::Basename        qw/ dirname /;
    use File::Spec::Functions qw/ catdir catfile /;

    sub setup {
        my ($self) = @_;
        $self->{partials} = catdir(dirname(__FILE__), '..', 'partials');
        mkdir($self->{partials});
        Template::Mustache->CONFIG(template_path => $self->{partials});
    };

    sub setup_partials {
        my ($self, $test) = @_;
        for my $name (keys %{$test->{partials} || {}}) {
            my $filename = catfile($self->{partials}, "$name.mustache");
            open *FH, '>', $filename;
            print FH $test->{partials}->{$name};
            close FH;
        }
    }

    sub setup_data {
        my ($self, $test) = @_;
        return unless $test->{data};
        for my $key (keys %{$test->{data}}) {
            my $value = $test->{data}->{$key};
            next unless ref $value;

            if (ref $value eq 'code') {
                $test->{data}->{$key} = \&$value;
            } elsif (ref $value eq 'HASH') {
                $self->setup_data({ data => $value });
            }
        }
    }

    sub teardown {
        my ($self) = @_;
        rmtree($self->{partials});
    }
}

sub assert_mustache_spec {
    my ($test) = @_;

    my $expected = $test->{expected};
    my $tmpl = $test->{template};
    my $data = $test->{data};

    my $actual = Template::Mustache->render($tmpl, $data);

    assert_equal($actual, $expected,
        "$test->{desc}\n".
        "Data:     @{[ Dumper $test->{data} ]}\n".
        "Template: @{[ Dumper $test->{template} ]}\n".
        "Partials: @{[ Dumper ($test->{partials} || {}) ]}\n"
    );
}

for my $file (glob catfile(dirname(__FILE__), '..', 'specs', '*.yml')) {
    my $spec = YAML::XS::LoadFile($file);
    my $pkg = ucfirst(basename($file));

    no strict 'refs';
    @{"$pkg\::ISA"} = 'MustacheSpec';

    for my $test (@{$spec->{tests}}) {
        (my $name = $test->{name}) =~ s/'/"/g;

        *{"$pkg\::test - @{[$name]}"} = sub {
            my ($self) = @_;
            $self->setup_partials($test);
            $self->setup_data($test);
            assert_mustache_spec($test);
        };
    }
}
