def format_for_operator(text: str, options=None, reply_instruction: str = None) -> str:
    """Return a single copyable fenced block for operator messages.

    - text: short status or prompt
    - options: list of option strings (each will be placed on its own line)
    - reply_instruction: single-line instruction at the end
    """
    lines = ['```']
    if text:
        lines.append(text)
    if options:
        for o in options:
            lines.append(o)
    if reply_instruction:
        lines.append(reply_instruction)
    lines.append('```')
    return '\n'.join(lines)


if __name__ == '__main__':
    sample = format_for_operator('Please choose:', ['A) Yes', 'B) No'], 'Reply format: Choice=<A|B>')
    print(sample)
