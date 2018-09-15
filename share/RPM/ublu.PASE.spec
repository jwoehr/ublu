# This is a spec file for installing Ublu native on IBM i under PASE
 
%define _topdir     /home/jax/rpmbuild
%define name            ublu 
%define release     1
%define version     1.1.9+
%define buildroot %{_topdir}/%{name}-%{version}-root
%define pkgroot %{buildroot}/QOpenSys/pkgs
%define ubluroot %{pkgroot}/opt/ublu
%define ublulib %{ubluroot}/lib
%define ublushare %{ubluroot}/share
%define ubludoc %{ubluroot}/userdoc
%define ubluexamp %{ubluroot}/examples
%define ubluext %{ubluroot}/extensions
%define ubluscript %{ubluroot}/scripts
%define bindir %{pkgroot}/bin
%define mandir %{pkgroot}/man
 
BuildRoot:  %{buildroot}

Name:           %{name}
Version:        %{version}
Release:        %{release}
Summary:        Ublu Midrange and Mainframe Life Cycle Extension Language
License:        BSD-2
Source:         %{name}-%{version}.tar.gz
Prefix:         /QOpenSys/pkgs
Group:          Development/Tools
 
%description                                                                             
Ublu allows you to write programs which locally or remotely control IBM i and other platforms.
 
%prep
%setup -q

# %build
make -f share/RPM/Makefile

%install
mkdir -p %{ubluroot}
mkdir -p %{bindir}
mkdir -p %{mandir}
mkdir -p %{mandir}/man1
mkdir -p %{ublulib}
mkdir -p %{ubluexamp}
mkdir -p %{ubluexamp}/test
mkdir -p %{ubluexamp}/test/suite
mkdir -p %{ubluexamp}/pigiron
mkdir -p %{ubluexamp}/pigiron/test                                        
mkdir -p %{ubluext}
mkdir -p %{ubluext}/sysshep
mkdir -p %{ublushare} 
mkdir -p %{ublushare}/atom                
mkdir -p %{ublushare}/atom/language-ublu
mkdir -p %{ublushare}/atom/language-ublu/grammars
mkdir -p %{ublushare}/atom/language-ublu/spec
mkdir -p %{ublushare}/atom/language-ublu/lib
mkdir -p %{ublushare}/atom/language-ublu/lib/flyover
mkdir -p %{ublushare}/atom/language-ublu/snippets
mkdir -p %{ublushare}/atom/language-ublu/styles
mkdir -p %{ublushare}/jEdit
mkdir -p %{ublushare}/mssql
mkdir -p %{ublushare}/perl                                  
mkdir -p %{ublushare}/RPM
mkdir -p %{ublushare}/tn5250j
mkdir -p %{ublushare}/ublu-vimfiles-master
mkdir -p %{ublushare}/ublu-vimfiles-master/indent
mkdir -p %{ublushare}/ublu-vimfiles-master/syntax
mkdir -p %{ublushare}/ublu-vimfiles-master/ftdetect
mkdir -p %{ubluscript}
mkdir -p %{ubludoc}
mkdir -p %{ubludoc}/images

make -f share/RPM/Makefile install prefix=$RPM_BUILD_ROOT/QOpenSys/pkgs
  
%files
%defattr(-,qsys,0,-)
/QOpenSys/pkgs/man/man1/ublu.1.gz
/QOpenSys/pkgs/bin/ublu
/QOpenSys/pkgs/opt/ublu
