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
%define bindir %{pkgroot}/bin
 
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
mkdir -p %{ublulib}
mkdir -p %{ublushare}
mkdir -p %{ubludoc}
mkdir -p %{ubluexamp}
mkdir -p %{ubluext}
mkdir -p %{bindir}

make -f share/RPM/Makefile install prefix=$RPM_BUILD_ROOT/QOpenSys/pkgs
  
%files
%defattr(-,qsys,-,-)
/bin/ublu
/opt/ublu
/opt/ublu/*

%doc %attr(0555,root,root) /opt/ublu/userdoc
%doc %attr(0444,root,root) /opt/ublu/userdoc/*
%doc %attr(0555,root,root) /opt/ublu/lib
%doc %attr(0444,root,root) /opt/ublu/lib/*
%doc %attr(0555,root,root) /opt/ublu/lib/images
%doc %attr(0444,root,root) /opt/ublu/lib/images/*
%doc %attr(0555,root,root) /opt/ublu/share
%doc %attr(0444,root,root) /opt/ublu/share/*