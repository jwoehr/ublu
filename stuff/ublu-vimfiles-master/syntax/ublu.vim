" Vim syntax file
" Language: Ublu scripts
" Maintainer: https://github.com/Taywee
" Latest Revision: 2016-09-29
" Revised by jwoehr@softwoehr.com 2016 October 06

if exists("b:current_syntax")
    finish
endif

let b:current_syntax = "ublu"

syn keyword basicKeywords CALL CATCH LOCAL TASK BREAK RETURN TASK THROW TRY
syn keyword repeat DO FOR in IN WHILE
syn keyword function FUN FUNC
syn keyword conditional IF THEN ELSE SWITCH
syn keyword commandKeywords as400 ask bye calljava collection commandcall const cs db dbug defun dict dpoint dq eval exit file ftp gensh help histlog history h host ifs interpret include jmx job joblist joblog jrnl json jvm license lifo list monitor msg msgq num objdesc objlist outq ppl printer programcall props put record rs savf savef server sess session smapi sock spoolf spoolflist string subsys system sysval test thread tn5250 trace tuple usage user userlist
syn keyword constants AUTOSTART BATCH INTERACTIVE SUBSYSTEM_MONITOR SPOOLED_READER SYSTEM SPOOLED_WRITER SCPF_SYSTEM ALL

syn match number '\<[0-9]\+\>'
syn match number '\<0x[0-9a-fA-F]\+\>'
syn match number '\<[0-9]\+\.[0-9]\+\>'
syn match number '\<[0-9]\+e[0-9]\+\>'
syn match number '\<[0-9]\+\.[0-9]\+e[0-9]\+\>'

syn match option '-\S\+'
syn match tuple '@\S\+'


syn match comment "#.*$"
syn match shebang "^#!.*$"

syn match todo "\ctodo" containedin=comment contained

syn match bang "!\s*[0-9]\+"


syn match toOrFrom '\<-to\>' nextgroup=tuple skipwhite
syn match toOrFrom '\<-from\>' nextgroup=tuple skipwhite

syn region block start='\$\[' end='\]\$' fold transparent

syn region string start='\${' end='}\$'

syn region funcArgs start='(' end=')'

hi def link toOrFrom Special
hi def link option Label
hi def link basicKeywords Keyword
hi def link repeat Repeat
hi def link function Keyword
hi def link conditional Conditional
hi def link commandKeywords Statement
hi def link deprecatedKeywords Error
hi def link tuple Identifier
hi def link comment Comment
hi def link todo Todo
hi def link shebang SpecialComment
hi def link bang Special
hi def link string String
hi def link number Number
hi def link constants Constant
