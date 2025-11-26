export function scrollToElementInContainer(
  container: HTMLElement,
  element: HTMLElement,
  offset = 0,
) {
  setTimeout(() => {
    const containerTop = container.scrollTop
    const containerBottom = containerTop + container.clientHeight
    const elementTop = element.offsetTop - offset
    const elementBottom = elementTop + element.clientHeight + offset

    if (elementTop < containerTop) {
      container.scrollTo({ top: elementTop, behavior: 'smooth' })
    }
    else if (elementBottom > containerBottom) {
      container.scrollTo({ top: elementBottom - container.clientHeight, behavior: 'smooth' })
    }
  }, 0)
}
