export function scrollToElementInContainer(
  container: HTMLElement,
  element: HTMLElement,
  offset = 0,
) {
  setTimeout(() => {
    const containerTop = container.scrollTop
    const containerBottom = containerTop + container.clientHeight
    const elementTop = element.offsetTop
    const elementBottom = elementTop + element.clientHeight

    if (elementTop < containerTop) {
      container.scrollTo({ top: elementTop - offset, behavior: 'smooth' })
    }
    else if (elementBottom > containerBottom) {
      container.scrollTo({ top: elementBottom - container.clientHeight + offset, behavior: 'smooth' })
    }
  }, 0)
}
