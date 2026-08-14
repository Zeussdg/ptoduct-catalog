// Express controller'larda try/catch tekrarını önler: hatayı errorHandler'a iletir.
export const asyncHandler = (fn) => (req, res, next) => {
  Promise.resolve(fn(req, res, next)).catch(next);
};
