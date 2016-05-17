" Vim indent file
" Language: Ublu scripts
" Maintainer: https://github.com/Taywee
" Latest Revision: 2016 May 17 
"
if exists("b:did_indent")
    finish
endif
let b:did_indent = 1

setlocal indentexpr=GetUbluIndent()

setlocal indentkeys+=0=]$

setlocal autoindent

if exists("*GetUbluIndent")
    finish
endif

function! GetUbluIndent()
    " Find a non-blank line above the current line.
    let prevlnum = prevnonblank(v:lnum - 1)

    " Hit the start of the file, use zero indent.
    if prevlnum == 0
        return 0
    endif

    " Add a 'shiftwidth' after lines that start a block:
    " 'function', 'if', 'for', 'while', 'repeat', 'else', 'elseif', '{'
    let line = getline(v:lnum)
    let ind = indent(prevlnum)
    let prevline = getline(prevlnum)

    let lcount = len(split(prevline, '\$\[', 1)) - 1
    let rcount = len(split(prevline, '\]\$', 1)) - 1
    let shiftcount = lcount - rcount
    let shift = &shiftwidth * shiftcount

    let midx = match(line, '\(\]\$\|\$\[\)')

    if synIDattr(synID(prevlnum, midx + 1, 1), "name") != "comment" && prevline != ']$'
        let ind = ind + shift
    endif

    " Subtract a 'shiftwidth' on ]$
    " This is the part that requires 'indentkeys'.
    let midx = match(line, '^\s*\]\$\s*')
    if midx != -1
        let ind = ind - &shiftwidth
    endif

    return ind
endfunction
